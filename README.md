**WARNING**  
There have been reports of increased billing by AWS.    
This might be caused by a bug either in this module or within Liferay DXP/CE.  
So far I wasn't able to reproduce but included some additional measures trying to prevent this:  

- webcontent version should be < 2.0
- webcontent will not be translated once it has been translated for 3x waittime (3x 10 seconds by default)
- triggertag is explicitly removed

**INSTRUCTIONS**  

Current version is for Liferay DXP 7.3
Older versions can be found in the branches.

Check AWS Translate for the exact configuration.  
First make sure to add the following properties to your portal-ext.properties or in the console:

```
aws.accessKeyId=AKIA....
aws.secretKey=aa....
aws.region=eu-west-1
translate.fields=content,fieldA,fieldB
```


You can use this module in two ways:

1. Add a tag 'autotranslate' to your webcontent item and it will translate the title from en_US to each enabled language
and the same for all fields configured in `translate.fields` in your portal.properties


2. Use it realtime (need to add some caching) in webcontent templates

**WARNING**: Before you can use the following snippet make sure the "serviceLocator" is not under your freemarker restricted values. To check that go to Control panel >> System settings >> template engine and under "free marker engine" , remove the "serviceLocator" from restricted values.

```
<#if locale.getLanguage() == "en" >
  ${.vars['reserved-article-title'].data}
<#else>
  <#assign translateService = serviceLocator.findService("com.liferay.demo.auto.translate.api.TranslateService") /> 
  ${translateService.doTranslate("en",locale.getLanguage(),.vars['reserved-article-title'].data)} 
</#if>
```
