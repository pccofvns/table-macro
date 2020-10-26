package com.hcentive.exchange.docs.asciidoc.macro.processor;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.extension.JavaExtensionRegistry;
import org.asciidoctor.jruby.extension.spi.ExtensionRegistry;

/**
 * 
 * Extends the AsciiDoc syntax to support a table macro. It can create an asciidoc table from the sql query provided in its parameters. 
 * Sometimes default query is exectuted which needs to be passed via asciidoc attributes
 * 
 * Usage:
 * 
 * table::MEMBER[query="ddl", columnDataStyle="c,c,p,c,p"]
 *
 * table::batch_job_type[query="select attribute_name as Name, attribute_value as Value from config order by attribute_name", columnDataStyle="c,c,p"]
 *
 * 
 * @author Prashant C Chaturvedi
 *
 */
public class TableBlockMacroExtension implements ExtensionRegistry{

	@Override
	public void register(Asciidoctor asciidoctor) {
		JavaExtensionRegistry javaExtensionRegistry = asciidoctor.javaExtensionRegistry();
        javaExtensionRegistry.blockMacro("table", TableBlockProcessor.class);
		
	}

}
