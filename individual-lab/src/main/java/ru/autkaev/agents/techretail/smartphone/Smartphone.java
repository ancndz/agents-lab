package ru.autkaev.agents.techretail.smartphone;

import java.io.Serializable;
import java.util.Objects;

/**
 * Модель смартфона.
 *
 * @author Anton Utkaev
 * @since 2022.06.12
 */
public class Smartphone implements Serializable {

    /**
     * Наименование модели.
     */
    private String name;

    /**
     * Кол-во памяти.
     */
    private Integer installedRam;

    /**
     * Частота процессора.
     */
    private Double cpuSpeed;

    /**
     * Тип ОС.
     */
    private SmartphoneOs smartphoneOs;

    /**
     * Цена.
     */
    private Double price;

    public Smartphone() {
    }

    public String getName() {
        return name;
    }

    public Integer getInstalledRam() {
        return installedRam;
    }

    public Double getCpuSpeed() {
        return cpuSpeed;
    }

    public SmartphoneOs getSmartphoneOs() {
        return smartphoneOs;
    }

    public Double getPrice() {
        return price;
    }

    @Override
    public String toString() {
        return String.format(
                "Smartphone: [name: %s], [installedRam: %s], [cpuSpeed: %s], [smartphoneOs: %s], [price: %s]",
                getName(),
                getInstalledRam(),
                getCpuSpeed(),
                getSmartphoneOs() != null ? getSmartphoneOs().getOsName() : null,
                getPrice());
    }

    public Smartphone setName(String name) {
        this.name = name;
        return this;
    }

    public Smartphone setNonNullName(String name) {
        return setName(Objects.requireNonNull(name));
    }

    public Smartphone setInstalledRam(Integer installedRam) {
        this.installedRam = installedRam;
        return this;
    }

    public Smartphone setInstalledRam(String installedRam) {
        try {
            return setInstalledRam(Integer.parseInt(installedRam));
        } catch (NumberFormatException numberFormatException) {
            this.installedRam = null;
        }
        return this;
    }

    public Smartphone setCpuSpeed(Double cpuSpeed) {
        this.cpuSpeed = cpuSpeed;
        return this;
    }

    public Smartphone setCpuSpeed(String cpuSpeed) {
        try {
            return setCpuSpeed(Double.parseDouble(cpuSpeed));
        } catch (NumberFormatException e) {
            this.cpuSpeed = null;
        }
        return this;
    }

    public Smartphone setSmartphoneOs(SmartphoneOs smartphoneOs) {
        this.smartphoneOs = smartphoneOs;
        return this;
    }

    public Smartphone setPrice(Double price) {
        this.price = price;
        return this;
    }

    public Smartphone setNonNullPrice(Double price) {
        return setPrice(Objects.requireNonNull(price));
    }
}
