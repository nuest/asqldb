/*
 * This is part of Geomajas, a GIS framework, http://www.geomajas.org/.
 *
 * Copyright 2008-2012 Geosparc nv, http://www.geosparc.com/, Belgium.
 *
 * The program is available in open source according to the GNU Affero
 * General Public License. All contributions in this program are covered
 * by the Geomajas Contributors License Agreement. For full licensing
 * details, see LICENSE.txt in the project root.
 */
package org.hsqldb.hibernate.geomajas.layer.hibernate;

import org.hibernate.dialect.HSQLDialect;
import org.hibernate.usertype.UserType;
import org.hibernatespatial.SpatialDialect;
import org.hibernatespatial.SpatialFunction;


/**
 * HSQL implementation of spatial dialect. Has only one capability : geometry
 * user type.
 * 
 * @author Jan De Moerloose
 */
public class HSQLSpatialDialect extends HSQLDialect implements SpatialDialect {

	public String getDbGeometryTypeName() {
		return "VARCHAR";
	}

	public UserType getGeometryUserType() {
		return new HSQLGeometryUserType();
	}

	public String getSpatialAggregateSQL(String columnName, int aggregation) {
		throw new IllegalArgumentException(
				"Spatial aggregation is not known by this dialect");
	}

	public String getSpatialFilterExpression(String columnName) {
		throw new IllegalArgumentException(
				"Spatial filtering is not known by this dialect");
	}

	public String getSpatialRelateSQL(String columnName, int spatialRelation,
			boolean hasFilter) {
		throw new IllegalArgumentException(
				"Spatial relation is not known by this dialect");
	}

	public boolean isTwoPhaseFiltering() {
		return false;
	}

	@Override
	public String getDWithinSQL(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getHavingSridSQL(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getIsEmptySQL(String arg0, boolean arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSpatialRelateSQL(String arg0, int arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean supports(SpatialFunction arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supportsFiltering() {
		// TODO Auto-generated method stub
		return false;
	}


}
