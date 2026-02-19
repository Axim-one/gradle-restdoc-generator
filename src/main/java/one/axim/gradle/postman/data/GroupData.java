/*
 * Copyright ⓒ 2017 Brand X Corp. All Rights Reserved
 */
package one.axim.gradle.postman.data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;

/**
 * 포스트맨 그룹 데이터
 *
 * @author 황예원
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class GroupData {
    private String name;
    private ArrayList<ItemData> item;

    private String id;

    public GroupData() {

    }

    public GroupData(String name) {
        this.name = name;
        this.item = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public GroupData setName(String name) {
        this.name = name;
        return this;
    }

    public ArrayList<ItemData> getItem() {
        return item;
    }

    public GroupData setItem(ArrayList<ItemData> item) {
        this.item = item;
        return this;
    }

    public void addItem(ItemData item) {

        this.item.add(item);
    }

    public String getId() {
        return id;
    }

    public GroupData setId(String id) {
        this.id = id;
        return this;
    }
}