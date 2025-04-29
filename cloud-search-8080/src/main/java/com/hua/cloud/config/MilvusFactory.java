package com.hua.cloud.config;

import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * Author: Hua
 * Date: 2024/6/24 20:57
 */
@Component
public class MilvusFactory implements PooledObjectFactory<MilvusClientV2> {

    @Resource
    private ConnectConfig connectConfig;

    @Override
    public PooledObject<MilvusClientV2> makeObject(){
        MilvusClientV2 milvusClientV2 = new MilvusClientV2(connectConfig);
        return new DefaultPooledObject<>(milvusClientV2);
    }

    @Override
    public void destroyObject(PooledObject<MilvusClientV2> pooledObject) throws Exception {
        MilvusClientV2 milvusClientV2 = pooledObject.getObject();
        milvusClientV2.close(10);
    }

    @Override
    public boolean validateObject(PooledObject<MilvusClientV2> pooledObject) {
        //检测一个对象是否有效,在从对象池获取对象或归还对象到对象池时，会调用这个方法，判断对象是否有效，如果无效就会销毁。
        MilvusClientV2 client = pooledObject.getObject();
        try {
            client.listDatabases();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void activateObject(PooledObject<MilvusClientV2> pooledObject) throws Exception {
        //激活一个对象或者说启动对象的某些操作。
    }

    @Override
    public void passivateObject(PooledObject<MilvusClientV2> pooledObject) throws Exception {
        //钝化一个对象。在向对象池归还一个对象是会调用这个方法。这里可以对对象做一些清理操作。比如清理掉过期的数据，下次获得对象时，不受旧数据的影响。
    }
}
