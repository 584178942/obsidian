package com.siyu.mdm.custom.device.json;

import java.util.List;

/**
 * @author ZT
 */
public class Bean {
    /**
     * 命令分类
     */
    private int type;

    /**
     * 图片url
     */
    private String imageUrl;

    /**
     * apk更新url
     */
    private String apkUrl;

    /**
     * apk包名
     */
    private String pkgName;

    /**
     * 包名list
     */
    private List<pkgList> pkgList;


    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getApkUrl() {
        return apkUrl;
    }

    public void setApkUrl(String apkUrl) {
        this.apkUrl = apkUrl;
    }

    public String getPkgName() {
        return pkgName;
    }

    public void setPkgName(String pkgName) {
        this.pkgName = pkgName;
    }

    public List<Bean.pkgList> getPkgList() {
        return pkgList;
    }

    public void setPkgList(List<Bean.pkgList> pkgList) {
        this.pkgList = pkgList;
    }


    /**
     * 白名单包名类
     */
    public static class pkgList{
        /**
         * 包名
         */
        private String  pkgName;
        public String getPkgName() {
            return pkgName;
        }

        public void setPkgName(String pkgName) {
            this.pkgName = pkgName;
        }
    }
}
