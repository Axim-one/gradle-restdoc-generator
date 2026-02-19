package one.axim.gradle.data;

public class ENVDefinition {

    private String devDescription;
    private String devServerHost;
    private String stagDescription;
    private String stagServerHost;
    private String prodDescription;
    private String prodServerHost;

    public String getDevDescription() {
        return devDescription;
    }

    public void setDevDescription(String devDescription) {
        this.devDescription = devDescription;
    }

    public String getDevServerHost() {
        return devServerHost;
    }

    public void setDevServerHost(String devServerHost) {
        this.devServerHost = devServerHost;
    }

    public String getStagDescription() {
        return stagDescription;
    }

    public void setStagDescription(String stagDescription) {
        this.stagDescription = stagDescription;
    }

    public String getStagServerHost() {
        return stagServerHost;
    }

    public void setStagServerHost(String stagServerHost) {
        this.stagServerHost = stagServerHost;
    }

    public String getProdDescription() {
        return prodDescription;
    }

    public void setProdDescription(String prodDescription) {
        this.prodDescription = prodDescription;
    }

    public String getProdServerHost() {
        return prodServerHost;
    }

    public void setProdServerHost(String prodServerHost) {
        this.prodServerHost = prodServerHost;
    }
}
