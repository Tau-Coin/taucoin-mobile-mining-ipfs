package io.taucoin.datasource.redis;

public interface RedisSerializer<T> {

    boolean canSerialize(Object o);

    byte[] serialize(T t);

    T deserialize(byte[] bytes);
}
