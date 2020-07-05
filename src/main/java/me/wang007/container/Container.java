package me.wang007.container;


import java.lang.annotation.Annotation;
import java.util.List;

/**
 * 组件容器， 通过该容器获取组件
 *
 * created by wang007 on 2019/2/25
 */
public interface Container {

    ClassLoader Default_ClassLoader = ClassLoader.getSystemClassLoader();

    /**
     * 根据给定的目标类， 获取组件
     *
     * @param targetClz 目标类
     * @return 指定的{@link Component}
     */
    Component getComponent(Class<?> targetClz);


    /**
     * 根据给定的被目标注解 注解的类， 获取组件
     *
     * @param loadBy 目标注解
     * @return 指定的{@link Component} 集合
     */
    List<Component> getComponentsByAnnotation(Class<? extends Annotation> loadBy);


//    /**
//     * 根据给定的类，获取其子类的组件。
//     * 不获取指定类，如果需要获取指定类，可以用{@link #getComponent(Class)}
//     *
//     * @param fromClz 目标类
//     * @return 指定的{@link Component} 集合
//     */
//    List<Component> getComponentsFrom(Class<?> fromClz);


    /**
     * 指定加载的类 是否被目标注解 注解上。
     *
     * note: 如果已加载过组件，则不做加载操作。
     *
     * @param loadBy 目标注解
     * @return this
     */
    Container registerLoadBy(Class<? extends Annotation> loadBy);

    /**
     * 启动container，加载组件
     *
     * @param basePaths 组件的基路径
     * @return this
     */
    Container start(String... basePaths);

    /**
     * 容器是否完成启动
     *
     * @return true：未启动， false: 已启动
     */
    boolean started();

}
