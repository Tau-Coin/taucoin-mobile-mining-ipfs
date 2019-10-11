package io.taucoin.datasource.redis;

import io.taucoin.config.SystemProperties;
import io.taucoin.core.Transaction;
import io.taucoin.datasource.KeyValueDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;

import javax.annotation.PreDestroy;
import javax.inject.Singleton;

import java.net.URI;
import java.util.Map;
import java.util.Set;

@Singleton
public class RedisConnectionImpl implements RedisConnection {

    private static final Logger logger = LoggerFactory.getLogger("db");

    private JedisPool jedisPool;

	public RedisConnectionImpl(){
        tryConnect();
    }

    public void tryConnect() {
        if (!SystemProperties.CONFIG.isRedisEnabled()) return;

        String redisCloudUrl = System.getenv(REDISCLOUD_URL);
        if (redisCloudUrl.isEmpty()) {
            logger.info("Cannot connect to Redis. 'REDISCLOUD_URL' environment variable is not defined.");
            return;
        }

        logger.info("Redis pool creating: " + redisCloudUrl);
        try {
            jedisPool = createJedisPool(new URI(redisCloudUrl));
        } catch (Exception e) {
            logger.warn("Cannot connect to Redis cloud: ", e);
        } finally {
            logger.info(isAvailable() ? "Redis cloud connected successfully." : "Redis cloud connection failed.");
        }
    }

    private static boolean hasText(String string) {
        return string != null && !string.isEmpty() && !string.trim().isEmpty();
    }

    private static JedisPool createJedisPool(URI redisUri) {
        String userInfo = redisUri.getUserInfo();
        if (hasText(userInfo)) {
            return new JedisPool(new JedisPoolConfig(),
                    redisUri.getHost(),
                    redisUri.getPort(),
                    Protocol.DEFAULT_TIMEOUT,
                    userInfo.split(":", 2)[1]);
        }

        return new JedisPool(new JedisPoolConfig(),
                redisUri.getHost(),
                redisUri.getPort(),
                Protocol.DEFAULT_TIMEOUT);
    }

    @PreDestroy
    public void destroy() {
        if (jedisPool != null) {
            jedisPool.destroy();
        }
    }

    @Override
    public boolean isAvailable() {
        boolean available = jedisPool != null;
        if (available) {
            try {
                Jedis jedis = jedisPool.getResource();
                try {
                    available = jedis.isConnected();
                } finally {
                    jedisPool.returnResource(jedis);
                }
            } catch (Throwable t) {
                logger.warn("Connection testing failed: ", t);
                available = false;
            }
        }
        return available;
    }

    @Override
    public <T> Set<T> createSetFor(Class<T> clazz, String name) {
        return new RedisSet<T>(name, jedisPool, Serializers.forClass(clazz));
    }
    
    @Override
    public <K,V> Map<K, V> createMapFor(Class<K> keyClass, Class<V> valueClass, String name) {
        return new RedisMap<K, V>(name, jedisPool, Serializers.forClass(keyClass), Serializers.forClass(valueClass));     
    }

    @Override
    public Set<Transaction> createTransactionSet(String name) {
        return createSetFor(Transaction.class, name);
    }

    @Override
    public KeyValueDataSource createDataSource(String name) {
        return new RedisDataSource(name, jedisPool);
    }
}
