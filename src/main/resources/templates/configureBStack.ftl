<html>
<head>
    <title>BrowserStack Configuration</title>
    <meta name="decorator" content="adminpage">
</head>
<body>
<img style="float:right" width="180px" src="${req.contextPath}/download/resources/com.browserstack.bamboo.browserstack-bamboo-integration:BStackAssets/browserstack_logo.svg" border="0"/>
<h1>BrowserStack Configuration</h1>

<div class="paddedClearer"></div>
    [@ww.form action="/admin/browserstack/BStackSaveConfiguration.action"
        id="BStackConfigurationForm"
        submitLabelKey='global.buttons.update'
        cancelUri='/admin/administer.action']
        [@ww.textfield name="username" label='Username' /]
        [@ww.password name="accessKey" label='Access Key' showPassword='true'/]

        [@ww.checkbox label='Enable BrowserStack Local' name='browserstackLocal' toggle='true' description='BrowserStack Local allows you to test your private and internal servers, alongside public URLs on BrowserStack, <a target="_blank" href="https://www.browserstack.com/local-testing">Learn more</a>.<br>Note: You can skip this if you are already using our local bindings.' /]
        [@ui.bambooSection dependsOn='browserstackLocal' showOn='true']
            [@ww.textfield name="browserstackLocalArgs" label='Modifiers' description='Any additional <a target="_blank" href="https://www.browserstack.com/local-testing#modifiers">configuration options</a>.(Optional)' /] 
        [/@ui.bambooSection]
        [@ww.checkbox label='Disable BrowserStack Environment Variables' name='disableEnvironmentVariables' toggle='true' description='By checking this box, you will disable BrowserStack specific environment variables, like the username and access key, which are appended to any environment variables you already pass' /]
    [/@ww.form]
</body>
</html>
