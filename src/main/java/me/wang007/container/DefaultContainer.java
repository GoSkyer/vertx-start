package me.wang007.container;


import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import dorkbox.annotation.AnnotationDefaults;
import dorkbox.annotation.AnnotationDetector;
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

        //获取class
        List classModules = null;
        try {
            classModules = AnnotationDetector.scanClassPath(basePaths)
                    .forAnnotations(loadByAnnotation.toArray(new Class[loadByAnnotation.size()]))  // one or more annotations
                    .collect(AnnotationDefaults.getType);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Map<Class<?>, Component> map =
                componentLoader.loadComponents(classModules, loadByAnnotation);
        map.forEach(componentMap::put);
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
            logger.info("basePaths=" + Arrays.toString(basePaths));
            loadComponents(basePaths);
            logger.info("container started completely");
        }
    }


}
