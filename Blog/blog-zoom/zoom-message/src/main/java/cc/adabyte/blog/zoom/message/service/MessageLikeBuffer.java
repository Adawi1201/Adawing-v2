package cc.adabyte.blog.zoom.message.service;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * 留言点赞增量缓冲器。
 * 点赞时在内存累加，由定时任务批量刷回 DB。缓冲的是「待刷回的脏增量」，
 * 不使用 Caffeine（其淘汰机制会丢失未刷回增量）。仿 {@code ViewCountBuffer}。
 */
@Component
public class MessageLikeBuffer {

    private final ConcurrentHashMap<Long, LongAdder> counters = new ConcurrentHashMap<>();

    /** 留言点赞内存 +1 */
    public void increment(Long id) {
        counters.computeIfAbsent(id, k -> new LongAdder()).increment();
    }

    /** 当前未刷回的增量（不清零） */
    public long peek(Long id) {
        LongAdder adder = counters.get(id);
        return adder == null ? 0L : adder.sum();
    }

    /**
     * 取出所有未刷回增量并清零，返回 delta &gt; 0 的项。
     * 注：{@code LongAdder.sumThenReset} 非严格原子，刷库瞬间极小概率丢失个位数增量，点赞数非关键数据可接受。
     */
    public Map<Long, Long> drain() {
        Map<Long, Long> result = new HashMap<>();
        for (Map.Entry<Long, LongAdder> entry : counters.entrySet()) {
            long delta = entry.getValue().sumThenReset();
            if (delta > 0) {
                result.put(entry.getKey(), delta);
            }
        }
        return result;
    }
}
