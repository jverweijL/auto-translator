Check AWS Translate for the exact configuration.  
First make sure to add the following properties to your portal-ext.properties or in the console:

```
aws.accessKeyId=AKIA....
aws.secretKey=aa....
```


You can use this module in two ways:

1. Add a tag 'autotranslate' to your webcontent item and it will translate the title from en_US to each enabled language

2. Use it realtime (need to add some caching) in webcontent templates

```
<#if locale.getLanguage() == "en" >
  ${.vars['reserved-article-title'].data}
<#else>
  <#assign translateService = serviceLocator.findService("com.liferay.demo.auto.translate.api.TranslateService") /> 
  ${translateService.doTranslate(locale.getLanguage(),.vars['reserved-article-title'].data)} 
</#if>
```