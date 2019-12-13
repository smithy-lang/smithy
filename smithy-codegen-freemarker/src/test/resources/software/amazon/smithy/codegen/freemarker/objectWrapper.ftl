Hello, ${name}
${empty!}:${present}
${object.foo}
${array[0]}
${number}
${boolean?c}
<#list stream as item>${item}<#if item?has_next>,</#if></#list>
