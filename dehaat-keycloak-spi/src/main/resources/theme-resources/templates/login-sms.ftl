<#import "custom_dehaat_template.ftl" as layout>
<@layout.registrationLayout displayInfo=true; section>
	<#if section = "header">
	         ${msg("smsAuthTitle")}
	<#elseif section = "form">
		<form id="kc-sms-code-login-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
			<div class="${properties.kcFormGroupClass!}">
                 <div class="${properties.kcLabelWrapperClass!}">
                        <label for="code" class="${properties.kcLabelClass!}">${msg("smsAuthLabel")}</label>
                </div>
				<div class="${properties.kcInputWrapperClass!}">
					<input type="text" id="code" name="otp" class="${properties.kcInputClass!}" autofocus/>
					<input type="hidden" id="mobile" name="mobile_number" value=${mobile_number}>
				</div>
			</div>
			<div class="${properties.kcFormGroupClass!} ${properties.kcFormSettingClass!}">
				<div id="kc-form-options" class="${properties.kcFormOptionsClass!}">
					<div class="${properties.kcFormOptionsWrapperClass!}">
						<span><a href="${url.loginRestartFlowUrl}">${kcSanitize(msg("backToLogin"))?no_esc}</a></span>
					</div>
				</div>

				<div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
					<input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}" type="submit" value="${msg("doSubmit")}"/>
				</div>
			</div>
		</form>
	<#elseif section = "info" >
		${msg("smsAuthInstruction")}
	</#if>
</@layout.registrationLayout>
