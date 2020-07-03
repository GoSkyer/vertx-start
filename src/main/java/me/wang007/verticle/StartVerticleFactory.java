package me.wang007.verticle;

import io.vertx.core.Promise;
import io.vertx.core.Verticle;
import io.vertx.core.spi.VerticleFactory;
import me.wang007.container.Container;

import java.util.concurrent.Callable;


/**
 * verticle factory, 覆盖默认的， 完成注入工作。
 *
 * created by wang007 on 2018/9/10
 */
public class StartVerticleFactory implements VerticleFactory {

    /**
     *
     */
    public static final String Start_Prefix = "start";



    @Override
    public String prefix() {
        return Start_Prefix;
    }

    @Override
    public void createVerticle(String verticleName, ClassLoader classLoader, Promise<Callable<Verticle>> promise)  {
        //must be the same type classLoader
        //ComponentParseImpl #loadClass method
        ClassLoader loader = Container.Default_ClassLoader;
        verticleName = VerticleFactory.removePrefix(verticleName);
        if(verticleName.endsWith(".java")) {
            throw new IllegalArgumentException("verticleName not support endWith java");
        }

        try {
            Class<?> clz = loader.loadClass(verticleName);
            Object v = clz.newInstance();
            promise.complete(() -> (Verticle) v);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
