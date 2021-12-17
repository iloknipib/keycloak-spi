<#import "custom_dehaat_template.ftl" as layout>
<@layout.registrationLayout displayInfo=true; section>
	<#if section = "header">
		${msg("smsAuthTitle",realm.displayName, "mobileLabel")}
	<#elseif section = "form">
		<form id="kc-sms-code-login-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
			<div class="${properties.kcFormGroupClass!}">

			    <div class="${properties.kcLabelWrapperClass!}">
                	<label for="mobile" class="${properties.kcLabelClass!}">${msg("mobileLabel")}</label>
                </div>
                <div class="${properties.kcInputWrapperClass!}">
                	<input type="text" id="mobile" name="mobile" class="${properties.kcInputClass!}" />
                </div>

				<div class="${properties.kcLabelWrapperClass!}">
					<label for="code" class="${properties.kcLabelClass!}">${msg("smsAuthLabel")}</label>
				</div>
				<div class="${properties.kcInputWrapperClass!}">
					<input type="text" id="code" name="otp" class="${properties.kcInputClass!}" />
				</div>
			</div>
			<div class="${properties.kcFormGroupClass!} ${properties.kcFormSettingClass!}">
				<div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
					<input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}" id="submit-android-btn" type="submit" value="${msg("doSubmit")}"/>
				</div>
			</div>
		</form>
	<#elseif section = "info" >
		${msg("smsAuthInstruction")}
	</#if>
</@layout.registrationLayout>
