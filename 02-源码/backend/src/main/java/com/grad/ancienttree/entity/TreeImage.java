package com.grad.ancienttree.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 古树影像资料（分类上传）。对应表 tree_image。
 */
@Data
@TableName("tree_image")
public class TreeImage {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联古树ID */
    private Long treeId;

    /** 分类：全景/树干/树冠/病害/养护前/养护后 */
    private String category;

    /** 相对路径 /uploads/... */
    private String url;

    /** 原始文件名 */
    private String originalName;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 上传人ID */
    private Long uploaderId;

    /** 上传时间 */
    private LocalDateTime createTime;
}
