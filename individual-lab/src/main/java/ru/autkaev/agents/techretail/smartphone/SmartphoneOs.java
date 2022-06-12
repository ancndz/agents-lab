package ru.autkaev.agents.techretail.smartphone;

/**
 * Типы операционных систем.
 *
 * @author Anton Utkaev
 * @since 2022.06.12
 */
public enum SmartphoneOs {

    IOS("ios"),

    ANDROID_4("Android4"),

    ANDROID_5("Android5"),

    FREE_OS("FreeOs");

    private final String osName;

    SmartphoneOs(final String osName) {
        this.osName = osName;
    }

    public String getOsName() {
        return osName;
    }

    @Override
    public String toString() {
        return getOsName();
    }
}
