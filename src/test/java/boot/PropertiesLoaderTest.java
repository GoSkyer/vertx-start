package boot;

import example.Profile;
import me.wang007.boot.PropertiesLoader;
import me.wang007.constant.VertxBootConst;
import me.wang007.container.DefaultContainer;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * created by wang007 on 2019/2/27
 */
public class PropertiesLoaderTest {


    @Test
    public void loadPropertiesTest() {

        PropertiesLoader propertiesLoader = new PropertiesLoader();

        ConcurrentHashMap<String, String> map = propertiesLoader.loadProperties(VertxBootConst.Default_Properties_Path).getProperties();
        Assert.assertEquals(map.get("name"), "wang007");
    }
}
