package me.wang007.router;

import io.vertx.core.Future;
import io.vertx.core.Vertx;


import io.vertx.ext.web.Router;
import me.wang007.annotation.Route;
import me.wang007.verticle.HttpServerVerticle;

/**
 * 用于装载{@link io.vertx.ext.web.Route}， 使用{@link LoadRouter} 标识是个router，在启动的进行扫描
 *
 * 多实例的verticle中，每个verticle中的LoadRouter、{@link Router}都是独立的。 强制放到一起，会发生并发问题。
 *
 * new {@code ->} init {@code ->} 对所有的order排序 {@code ->} start
 *
 *
 *
 * Created by wang007 on 2018/8/21.
 */
public interface LoadRouter {

    /**
     * {@link LoadRouter}生命周期方法。
     *
     */
    void start();

    /**
     *
     * @return 用于 {@link LoadRouter} 排序， 升序。 默认: 0.
     */
    default int order() {
        return 0 ;
    }


    /**
     * {@link LoadRouter}创建好后调用。
     *
     * 例如：权限相关的route实现，可以放到该方法中。
     *
     * @param router 当使用{@link Route#mountPath()} 挂载路径， router为subRouter, (子路由)
     * @param vertx vertx实例
     * @param server {@link HttpServerVerticle}定义client组件，然后在这里获取，达到所有LoadRouter共享。
     */
    default <T extends HttpServerVerticle> void init(Router router, Vertx vertx,  T server) {}

}
