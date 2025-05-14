package com.linkedout.recipe.entity;

import com.linkedout.common.type.UnitEnum;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.List;

@Table("recipe")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Recipe extends BaseEntity {
  @Id
  @Column("id")
  private Long id;

  @Column("name")
  private String name;

  @Column("main_image")
  private String mainImage;

  @Column("method")
  private String method;

  @Column("type")
  private String type;

  @Column("tip")
  private String tip;

  @Column("ingredients")
  private List<Ingredient> ingredients;

  @Column("sources")
  private List<Source> sources;

  @Column("manual_steps")
  private List<ManualStep> manualSteps;

  @Data
  public static class Ingredient {
    private String name;
    private UnitEnum unit;
    private String quantity;
  }

  @Data
  public static class Source {
    private String name;
    private UnitEnum unit;
    private String quantity;
  }

  @Data
  public static class ManualStep {
    private Integer id;
    private String text;
    private String image;
  }
}
