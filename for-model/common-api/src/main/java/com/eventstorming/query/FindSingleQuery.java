forEach: View
representativeFor: View
fileName: {{namePascalCase}}SingleQuery.java
path: common-api/{{{options.packagePath}}}/query
---
package {{options.package}}.query;

import lombok.Data;

@Data
public class {{namePascalCase}}SingleQuery {

{{#contexts.target.fieldDescriptors}}
    {{#isKey}}
        private {{className}} {{nameCamelCase}};
    {{/isKey}}
{{/contexts.target.fieldDescriptors}}

}


<function>
var me = this;

if(this.dataProjection == "query-for-aggregate"){
  var me = this;
  this.boundedContext.aggregates.forEach(agg => {if(agg.name==me.name) me.aggregate = agg});

  this.contexts.target = this.aggregate.aggregateRoot;
  this.contexts.readModelClass = this.contexts.target.namePascalCase + "ReadModel";
}else{
  this.contexts.target = this;
  this.contexts.readModelClass = this.contexts.target.namePascalCase;
}

</function>