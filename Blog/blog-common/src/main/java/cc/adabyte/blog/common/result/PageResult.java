package cc.adabyte.blog.common.result;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class PageResult<T> implements Serializable {
    private Long total;
    private List<T> list;
    private Long current;
    private Long size;

    public static <T> PageResult<T> of(Long total, List<T> list, Long current, Long size) {
        PageResult<T> r = new PageResult<>();
        r.setTotal(total);
        r.setList(list);
        r.setCurrent(current);
        r.setSize(size);
        return r;
    }
}
