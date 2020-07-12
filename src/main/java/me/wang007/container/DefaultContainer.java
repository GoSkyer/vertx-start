package me.wang007.container;


import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import me.wang007.annotation.Deploy;
import me.wang007.annotation.Route;
import me.wang007.exception.InitialException;

/**
 * created by wang007 on 2019/2/26
 */
public class DefaultContainer implements Container {

    protected static final Logger logger = LoggerFactory.getLogger(DefaultContainer.class);

    private static volatile DefaultContainer singleton = null;

    private final AtomicBoolean started = new AtomicBoolean(false);

    /**
     * 目标类被注解的集合
     */
    private final List<Class<? extends Annotation>> loadByAnnotation = new ArrayList<>();

    private final ComponentLoader componentLoader = new DefaultComponentLoader();

    private final Map<Class<?>, Component> componentMap = new HashMap<>();

    protected final List<Class<? extends Annotation>> getLoadByAnnotation() {
        return Collections.unmodifiableList(loadByAnnotation);
    }

    private Map<Class<?>, Component> componentMap() {
        return Collections.unmodifiableMap(componentMap);
    }

    private DefaultContainer(String... basePaths) {
        registerLoadBy(me.wang007.annotation.Properties.class).registerLoadBy(Deploy.class).registerLoadBy(Route.class);
        start(basePaths);
    }

    public static void init(String... basePaths) {
        if (singleton == null) {
            synchronized (DefaultContainer.class) {
                if (singleton == null) {
                    //通过Builder默认的配置来创建一个Picasso
                    singleton = new DefaultContainer(basePaths);
                }
            }
        }
    }

    public static DefaultContainer get() {
        if (singleton == null) {
            throw new InitialException("DefaultContainer has not been initialized");
        }
        return singleton;
    }


    @Override
    public Component getComponent(Class<?> targetClz) {
        Objects.requireNonNull(targetClz, "require not null");
        return componentMap().get(targetClz);
    }

    @Override
    public List<Component> getComponentsByAnnotation(Class<? extends Annotation> loadBy) {
        Objects.requireNonNull(loadBy, "require not null");
        List<Component> components = new ArrayList<>();
        componentMap().forEach((clz, component) -> {
            if (component.annotationBy(loadBy)) components.add(component);
        });
        return components;
    }

    @Override
    public boolean started() {
        return started.get();
    }


    //下面都是初始化用到的私有方法

    private DefaultContainer registerLoadBy(Class<? extends Annotation> loadBy) {
        Objects.requireNonNull(loadBy, "register require not null");
        synchronized (loadByAnnotation) {
            addIfAbsent(loadByAnnotation, loadBy);
        }
        return this;
    }

    private <E> void addIfAbsent(List<E> list, E obj) {
        if (!list.contains(obj)) list.add(obj);
    }

    private synchronized void loadComponents(String... basePaths) {

        Set<String> paths = new HashSet<>(Arrays.asList(basePaths));
        //TODO 一个basePath的路径， startWith 另一个basePath的路径， 那么这另一个路径是应该被剔除的

        if (basePaths.length == 0) {
            logger.warn("not found base path...");
        }

        Set<Class<?>> classes = new LinkedHashSet<>();
        //根据basePaths解析出class
        for (String path : paths) getClassesByPath(classes, path);

        Map<Class<?>, Component> map =
                componentLoader.loadComponents(classes, loadByAnnotation);
        map.forEach(componentMap::put);
    }


    /**
     * 递归加载class
     *
     * @param classes 装载class的容器
     * @param dotPath 以 "." 分割包路径的path
     */
    private void getClassesByPath(Set<Class<?>> classes, String dotPath) {
        Enumeration<URL> dirOrFiles = null;
        // 以 “/”分割的path
        String slashPath = dotPath.replace(".", "/");
        try {
            dirOrFiles = Thread.currentThread().getContextClassLoader().getResources(slashPath);
        } catch (IOException e) {
            logger.error("load class failed, path = {}", dotPath, e);
        }
        if (dirOrFiles == null) return; //加载文件或目录的路径出错

        while (dirOrFiles.hasMoreElements()) {
            URL dirOrFile = dirOrFiles.nextElement();
            //文件类型， file or jar
            String fileType = dirOrFile.getProtocol();

            if ("file".equals(fileType)) {
                String filePath = dirOrFile.getFile();
                File file = new File(filePath);

                if (!file.exists()) {
                    logger.warn("path: {}, file not exist", filePath);
                    continue;
                }
                //目录
                if (file.isDirectory()) {
                    File[] files = file.listFiles(f -> f.isDirectory() || f.getName().endsWith(".class"));
                    if (files == null) continue;

                    for (File f : files) {
                        String fileName = f.getName();
                        if (f.isDirectory()) getClassesByPath(classes, dotPath + '.' + fileName);
                        else if (f.getName().endsWith(".class")) {
                            //去掉 .class 结尾
                            fileName = fileName.substring(0, fileName.length() - 6);
                            Class<?> loadClass = loadClass(dotPath + '.' + fileName);
                            if (loadClass != null) {
                                boolean isExist = !classes.add(loadClass);
                                if (isExist) logger.error("class重复存在, {}", loadClass);
                            }
                        }
                    }
                    continue;
                }
                //class文件
                if (filePath.endsWith(".class")) {
                    int index = filePath.lastIndexOf("/");
                    //去掉 .class 结尾
                    String fileName = filePath.substring(index == -1 ? 0 : index + 1, filePath.length() - 6);
                    Class<?> loadClass = loadClass(fileName);
                    if (loadClass != null) {
                        boolean isExist = !classes.add(loadClass);
                        if (isExist) logger.error("class重复存在 in class, {}", loadClass);
                    }
                }

            } else if ("jar".equals(fileType)) {
                JarFile jar = null;
                try {
                    jar = ((JarURLConnection) dirOrFile.openConnection())
                            .getJarFile();
                } catch (IOException e) {
                    logger.warn("load classes failed... path -> {}", dotPath, e);
                }

                if (jar == null) continue;

                Enumeration<JarEntry> itemsForJar = jar.entries();
                while (itemsForJar.hasMoreElements()) {
                    JarEntry jarEntry = itemsForJar.nextElement();

                    /**
                     * 一个jar可能包括META-INF等其他非class文件。
                     *
                     * 这里扫描的目录和文件都会展开
                     * 即就是已经进行了递归到内层了
                     * 而且如果是目录， 以 “/” 结尾 忽略
                     * 如果是以 “.class” 结尾， 解析生成class
                     *
                     */
                    String fileName = jarEntry.getName();

                    //目录
                    if (fileName.endsWith("/")) continue;

                    if (fileName.charAt(0) == '/') {
                        fileName = fileName.substring(1);
                    }

                    //jar中文件或目录的路径，不与需要解析的路径匹配
                    if (!fileName.startsWith(slashPath)) continue;

                    //class文件
                    if (fileName.endsWith(".class") && !jarEntry.isDirectory()) {
                        //去掉 .class 结尾
                        String filePath = fileName.substring(0, fileName.length() - 6);
                        Class<?> loadClass = loadClass(filePath.replace('/', '.'));
                        if (loadClass != null) {
                            boolean isExist = !classes.add(loadClass);
                            if (isExist) logger.error("class重复存在 in jar, {}", loadClass);
                        }
                    }
                }

            } // fileType = jar

        } //foreach dirOrFiles
    }

    /**
     * 根据类路径， 加载 class
     *
     * @param dotPath 类路径
     * @return
     */
    private Class<?> loadClass(String dotPath) {
        logger.info("loadClass -> {}", dotPath);
        try {
            return Default_ClassLoader.loadClass(dotPath);
        } catch (ClassNotFoundException e) {
            logger.warn("load class failed, className -> {}", dotPath, e);
        }
        return null;
    }


    private void start(String... basePaths) {
        if (started.compareAndSet(false, true)) {
            logger.info("container starting...");
            loadComponents(basePaths);
            logger.info("container started completely");
        }
    }


}
