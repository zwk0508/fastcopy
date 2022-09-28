# fastcopy-bean属性快速复制

github地址：[https://github.com/zwk0508/fastcopy.git](https://github.com/zwk0508/fastcopy.git)

### 原理

使用asm的字节码技术，动态生成类，实现属性的复制，例如：

```java
public final class Generator$1$Copier implements Copier {
    public Generator$1$Copier() {
    }

    @Override
    public void copy(Object var1, Object var2) {
        User u1 = (User) var1;
        User u2 = (User) var2;
        u2.setUserId(u2.getUserId());
    }
}
```

### 速度

1. 比spring的BeanUtils快，快了两个数量级，初始化快了大约4~5倍
2. 比spring的BeanCopier快一点，初始化快了大约3倍
3. apache的就不比了

测试代码，dto的代码复制一份
```java
@Data
public class Attachment {

    private Integer attachId;

    private Integer projectId;

    private Integer layerId;

    private String attachName;

    private Integer attachDelStatus;

    private Integer isComplete;

    private Integer attachStatus;

    private String attachPath;

    private Double attachSize;

    private String attachEndfille;

    private String attachWebEndfile;

    private Double attachWebSize;

    private Integer showOrder;

    private String attachDoOrder;

    private String attachTrim;

    private String attachLocation;

    private String errorCode;

    private String attachTaskId;

    private Integer compTotal;

    private String attachCompPath;

    private Integer attachCompRetry;

    private String attachOsbimUrl;

    private String attachOsbUrl;

    private Double attachOsbSize;

    private Integer attachDoVersion;

    private Integer attachCategory;

    private String creator;

    private Timestamp createDate;

    private Timestamp updateDate;

    private String info;
}
```
```code
public void test() throws Exception {
    Attachment attachment = new Attachment();
    attachment.setAttachId(1);
    attachment.setProjectId(1);
    attachment.setLayerId(1);
    attachment.setAttachName("asdf");
    attachment.setAttachDelStatus(1);
    attachment.setIsComplete(1);
    attachment.setAttachStatus(1);
    attachment.setAttachPath("aaaa");
    attachment.setAttachSize(12356D);
    attachment.setAttachEndfille("12356D");
    attachment.setAttachWebEndfile("12356D");
    attachment.setAttachWebSize(12356D);
    attachment.setShowOrder(12356);
    attachment.setAttachDoOrder("12356");
    attachment.setAttachTrim("12356");
    attachment.setAttachLocation("12356");
    attachment.setErrorCode("12356");
    attachment.setAttachTaskId("12356");
    attachment.setCompTotal(12356);
    attachment.setAttachCompPath("12356");
    attachment.setAttachCompRetry(12356);
    attachment.setAttachOsbimUrl("12356");
    attachment.setAttachOsbUrl("12356");
    attachment.setAttachOsbSize(12356D);
    attachment.setAttachDoVersion(12356);
    attachment.setAttachCategory(12356);
    attachment.setCreator("12356");
    attachment.setCreateDate(new Timestamp(System.currentTimeMillis()));
    attachment.setUpdateDate(new Timestamp(System.currentTimeMillis()));
    attachment.setInfo("info");
    AttachmentDto dto = new AttachmentDto();
    for (int i = 0; i < 100; i++) {
        long start = System.nanoTime();
        FastCopyUtil.copy(attachment, dto);
        System.out.println(System.nanoTime() - start);
    }
    BeanCopier beanCopier = null;
    for (int i = 0; i < 100; i++) {
        long start = System.nanoTime();
        if (beanCopier == null) {
            beanCopier = BeanCopier.create(Attachment.class, AttachmentDto.class, true);
        }
        Converter c = (o, aClass, o1) -> o;
        beanCopier.copy(attachment, dto, c);
        System.out.println(System.nanoTime() - start);
    }
    for (int i = 0; i < 100; i++) {
        long start = System.nanoTime();
        BeanUtils.copyProperties(attachment, dto);
        System.out.println(System.nanoTime() - start);
    }
}
```

统计耗时，单位是纳秒
第一行是初始化耗时

|  fastcopy   | BeanCopier  | BeanUtils|
|  ----  | ----  | ----  |
| 30662500  | 87656500 | 129095100 |
| 3600  | 4700 | 616200 |
| 2800  | 3400 | 555000 |
| 2000  | 4800 | 497600 |
| 1800  | 6000 | 513800 |
| 1800  | 3600 | 428700 |
| 1900  | 3200 | 411700 |
| 1800  | 3300 | 401000 |
| 1800  | 4500 | 336400 |
| 1800  | 3100 | 318400 |
| 1700  | 3400 | 841100 |
| 1700  | 3700 | 655400 |
| 1600  | 4500 | 527500 |
| 1600  | 3300 | 733600 |
| 8000  | 3300 | 542700 |
| 3000  | 3400 | 9791400 |
| 2700  | 3300 | 1095600 |
| 2700  | 11700 | 501400 |
| 3000  | 3300 | 285400 |
| 2900  | 3300 | 335500 |
| 2700  | 3500 | 312900 |
| 2600  | 4800 | 338000 |
| 2600  | 3600 | 295700 |
| 2500  | 3300 | 277300 |
| 2500  | 3100 | 309100 |
| 2600  | 10300 | 296400 |
| 2600  | 3500 | 304400 |
| 2400  | 44600 | 237200 |
| 2600  | 3200 | 242500 |
| 2600  | 4000 | 711800 |
| 2600  | 3700 | 586600 |
| 2500  | 3600 | 247300 |
| 2500  | 3700 | 259300 |
| 2600  | 3200 | 231300 |
| 2600  | 4200 | 269300 |
| 2600  | 3200 | 209600 |
| 2700  | 3400 | 6221100 |
| 2300  | 3800 | 921200 |
| 2400  | 4000 | 169100 |
| 2400  | 3300 | 156400 |
| 2900  | 3900 | 161800 |
| 2600  | 3600 | 178400 |
| 2500  | 4600 | 152700 |
| 2400  | 3900 | 144700 |
| 2400  | 3600 | 143000 |
| 2300  | 3500 | 160200 |
| 3000  | 4400 | 158900 |
| 2700  | 3600 | 528400 |
| 2700  | 3300 | 175500 |
| 2600  | 3100 | 179500 |
| 2500  | 3100 | 213500 |
| 2600  | 4500 | 332600 |
| 2600  | 3500 | 163300 |
| 2600  | 3500 | 141900 |
| 2400  | 3500 | 1480900 |
| 2400  | 4100 | 654600 |
| 2900  | 3500 | 173800 |
| 3000  | 3900 | 134900 |
| 3000  | 3100 | 146600 |
| 3200  | 4700 | 148300 |
| 2800  | 3500 | 131200 |
| 2500  | 3600 | 132500 |
| 2600  | 3100 | 134300 |
| 2500  | 5000 | 132200 |
| 2700  | 3600 | 133000 |
| 3200  | 3500 | 138000 |
| 3800  | 3300 | 190300 |
| 3300  | 3800 | 131300 |
| 2700  | 4000 | 137000 |
| 2500  | 3900 | 163100 |
| 2700  | 3300 | 148900 |
| 2500  | 3600 | 153800 |
| 2400  | 4300 | 148300 |
| 2400  | 3700 | 146400 |
| 2500  | 4000 | 192900 |
| 2500  | 3200 | 158800 |
| 2300  | 3900 | 154500 |
| 2700  | 3700 | 149100 |
| 2900  | 3500 | 148700 |
| 3000  | 3500 | 189800 |
| 2700  | 3400 | 148100 |
| 2700  | 4100 | 173900 |
| 2700  | 3200 | 164800 |
| 2600  | 3600 | 143600 |
| 2600  | 3300 | 103800 |
| 2500  | 4300 | 107200 |
| 2500  | 3400 | 89100 |
| 2800  | 3300 | 89700 |
| 2700  | 3200 | 89300 |
| 2800  | 3800 | 92100 |
| 3000  | 3100 | 100100 |
| 2900  | 2900 | 109900 |
| 2600  | 3300 | 94200 |
| 2800  | 3900 | 84400 |
| 2700  | 3600 | 86400 |
| 2600  | 3700 | 133900 |
| 2600  | 4600 | 144300 |
| 2800  | 3900 | 121700 |
| 3300  | 4600 | 117400 |
| 2600  | 5000 | 121400 |