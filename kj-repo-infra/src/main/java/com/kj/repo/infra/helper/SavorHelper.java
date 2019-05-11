package com.kj.repo.infra.helper;

import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import com.google.common.base.CaseFormat;
import com.google.common.base.Strings;

import lombok.Data;

/**
 * 
 * @author kuojian21
 *
 */
public class SavorHelper {

	public static final Logger logger = LoggerFactory.getLogger(SavorHelper.class);

	public static RowMapper<Property> rowMapper = new RowMapper<Property>() {
		@Override
		public Property mapRow(ResultSet rs, int rowNum) throws SQLException {
			return new Property(rs);
		}
	};

	public static Model mysql(DataSource dataSource, String table) {
		List<Property> properties = new JdbcTemplate(dataSource)
						.query("show full columns from " + table, rowMapper);
		return new Model(table, properties);
	}

	public static void code(Model model) {
		for (String str : model.imports) {
			System.out.println("import " + str + ";");
		}

		System.out.println("/**");
		System.out.println(" * @author kj");
		System.out.println(" */");
		System.out.println("public class " + model.getName() + "{");
		for (Property property : model.getProperties()) {
			System.out.println("\t/*" + property.getComment() + "*/");
			if (property.isPrimaryKey()) {
				System.out.print("\t@Savor.PrimaryKey");
				if (property.isInsert()) {
					System.out.print("(insert=true)");
				}
				System.out.println();
			}
			System.out.println("\tprivate " + property.getType() + " " + property.getName() + ";");
		}
		System.out.println("}");
	}

	/**
	 * 
	 * @author kuojian21
	 *
	 */
	@Data
	public static class Model {
		private final String name;
		private final String table;
		private final List<Property> properties;
		private final Set<String> imports;

		public Model(String table, List<Property> properties) {
			super();
			this.name = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, table);
			this.table = table;
			this.properties = properties;
			this.imports = properties.stream().filter(p -> !Strings.isNullOrEmpty(p.getFullType()))
							.map(Property::getFullType).distinct()
							.collect(Collectors.toSet());
		}

	}

	/**
	 * 
	 * @author kuojian21
	 *
	 */
	@Data
	public static class Property {

		private final String name;
		private final String column;
		private final String type;
		private final String fullType;
		private final boolean primaryKey;
		private final boolean insert;
		private final String comment;

		public Property(ResultSet rs) throws SQLException {
			super();
			this.column = rs.getString("Field");
			this.name = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, column);
			String dbType = rs.getString("Type");
			if (dbType.startsWith("tinyint")
							|| dbType.startsWith("int")) {
				this.type = "Integer";
			} else if (dbType.startsWith("bigint")) {
				this.type = "Long";
			} else if (dbType.startsWith("varchar")
							|| dbType.endsWith("text")
							|| dbType.equals("json")) {
				this.type = "String";
			} else if (dbType.equals("timestamp")) {
				this.type = "java.sql.Timestamp";
			} else if (dbType.equals("date")) {
				this.type = "java.sql.Date";
			} else {
				this.type = "";
			}
			this.fullType = "";
			this.primaryKey = "PRI".equals(rs.getString("Key"));
			this.insert = !"auto_increment".equals(rs.getString("Extra"));
			this.comment = rs.getString("Comment");
		}

	}

}
