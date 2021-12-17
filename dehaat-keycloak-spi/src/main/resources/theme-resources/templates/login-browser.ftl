<#import "custom_dehaat_template.ftl" as layout>
<@layout.registrationLayout displayInfo=true; section>
	<#if section = "header">

	<#elseif section = "form">
		<form id="kc-sms-code-login-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
			<div class="${properties.kcFormGroupClass!}">

			    <div class="${properties.kcLabelWrapperClass!}">
                	<label for="mobile" class="${properties.kcLabelClass!}">${msg("mobileLabel")}</label>
                </div>
                <div class="${properties.kcInputWrapperClass!}">
                	<input type="text" id="username" name="username" class="${properties.kcInputClass!}" />
                </div>

			</div>
			<div class="${properties.kcFormGroupClass!} ${properties.kcFormSettingClass!}">
				<div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
					<input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}" id="submit-browser-btn" type="submit" value="${msg("doSubmit")}"/>
				</div>
			</div>
		</form>
	</#if>
</@layout.registrationLayout>
