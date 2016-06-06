package me.selinali.tribble.model;

import java.util.Date;

import lombok.Getter;

public class Shot {
  @Getter private int id;
  @Getter private String title;
  @Getter private String description;
  @Getter private int width;
  @Getter private int height;
  @Getter private Images images;
  @Getter private int viewsCount;
  @Getter private int likesCount;
  @Getter private int commentsCount;
  @Getter private User user;
  @Getter private Date createdAt;

  public class Images {
    @Getter private String hidpi;
    @Getter private String teaser;
    @Getter private String normal;

    public String getImage() {
      return hidpi == null? normal : hidpi;
    }
  }
}