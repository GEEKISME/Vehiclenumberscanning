package com.biotag.vehiclenumberscanning;

import com.google.gson.Gson;

/**
 * Created by Lxh on 2017/9/27.
 */

public class VersionUrlBean {

    /**
     * owner_version : {"no":"1.0.1","url":"http://211.152.45.196:8122/app_setup/CockOwner.apk"}
     */

    private OwnerVersionBean owner_version;

    public static VersionUrlBean objectFromData(String str) {

        return new Gson().fromJson(str, VersionUrlBean.class);
    }

    public OwnerVersionBean getOwner_version() {
        return owner_version;
    }

    public void setOwner_version(OwnerVersionBean owner_version) {
        this.owner_version = owner_version;
    }

    public static class OwnerVersionBean {
        /**
         * no : 1.0.1
         * url : http://211.152.45.196:8122/app_setup/CockOwner.apk
         */

        private String no;
        private String url;

        public static OwnerVersionBean objectFromData(String str) {

            return new Gson().fromJson(str, OwnerVersionBean.class);
        }

        public String getNo() {
            return no;
        }

        public void setNo(String no) {
            this.no = no;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }
}
