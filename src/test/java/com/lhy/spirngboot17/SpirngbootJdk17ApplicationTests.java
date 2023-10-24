package com.lhy.spirngboot17;

import com.lhy.spirngboot17.common.RedisLockUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@SpringBootTest
class SpirngbootJdk17ApplicationTests {

    @Autowired
    private RedisLockUtils redisLockUtils;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Test
    public void testLock() throws InterruptedException {
        String lockKey = "myLock";
        String requestId1 = "request1";
        String requestId2 = "request2";

        CountDownLatch latch = new CountDownLatch(2);

        // Simulate two threads trying to acquire the lock
        Thread thread1 = new Thread(() -> {
            try {
                if (redisLockUtils.lock(lockKey, requestId1, 1000, TimeUnit.SECONDS)) {
                    System.out.println("Thread 1: Lock acquired.");
                    Thread.sleep(2000); // Simulate some work with the lock


                    redisLockUtils.lock(lockKey, requestId1, 1000, TimeUnit.SECONDS);
                    System.out.println("Thread 1: Lock reacquired");

                    redisLockUtils.unlock(lockKey, requestId1);
                    System.out.println("Thread 1: Lock released.");
                } else {
                    System.out.println("Thread 1: Failed to acquire lock.");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                redisLockUtils.unlock(lockKey, requestId1);
                System.out.println("Thread 1: Lock finally released.");
                latch.countDown();
            }
        });

        Thread thread2 = new Thread(() -> {
            try {
                Thread.sleep(5000);
//                while (true){
                    if (redisLockUtils.lock(lockKey, requestId2, 10, TimeUnit.SECONDS)) {
                        System.out.println("Thread 2: Lock acquired.");
                        Thread.sleep(10000); // Simulate some work with the lock
                        redisLockUtils.unlock(lockKey, requestId2);
                        System.out.println("Thread 2: Lock released.");
                    } else {
                        System.out.println("Thread 2: Failed to acquire lock.");

                    }
//                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        });

        thread1.start();
        thread2.start();

        // Wait for both threads to finish
        latch.await();

        // Make assertions about the results if needed
    }
}
