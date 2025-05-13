package com.linkedout.recipe.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("recipe")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Recipe extends BaseEntity  {
	@Id
	@Column("id")
	private Long id;

	@Column("text")
	private String text;
}
