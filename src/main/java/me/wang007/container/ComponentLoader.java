package me.wang007.container;

import java.lang.annotation.Annotation;
import java.util.*;

/**
 * 组件加载器
 *
 * created by wang007 on 2019/2/26
 */
public interface ComponentLoader {

    /**
     * 从指定的class集合， 加载指定的组件。
     *
     * @param classes       class集合。
     * @param load          指定注解    即被加载的组件含有该注解
     * @param targetFrom    指定类型来源 即加载的class必须是 targetFrom的子类
     * @return 指定加载的组件
     */
    Map<Class<?>, Component> loadComponents(Collection<Class<?>> classes, List<Class<? extends Annotation>> load, Set<Class<?>> targetFrom);

//    /**
//     * 从指定的class集合， 加载指定的组件。
//     *
//     * @param classes class集合
//     * @param load    指定注解    即被加载的组件含有该注解
//     * @return 指定加载的组件
//     */
//    default Map<Class<?>, Component> loadComponents(Collection<Class<?>> classes,
//                                                    List<Class<? extends Annotation>> load) {
//        return loadComponents(classes, load, Collections.emptyList(), Collections.emptySet());
//    }

//    /**
//     * 从指定的class集合， 加载指定的组件。
//     *
//     * @param classes    class集合
//     * @param targetFrom 指定类型来源 即加载的class必须是 targetFrom的子类
//     * @return 指定加载的组件
//     */
//    default Map<Class<?>, Component> loadComponents(Collection<Class<?>> classes, Set<Class<?>> targetFrom) {
//        return loadComponents(classes, Collections.emptyList(), Collections.emptyList(), targetFrom);
//    }

}
