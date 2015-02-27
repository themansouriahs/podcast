package org.bottiger.podcast.flavors.CrashReporter;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface ReportCrashes {

    String formKey();

    String formUri();

    String formUriBasicAuthLogin();

    String formUriBasicAuthPassword();

    boolean disableSSLCertValidation();

    ReportingInteractionMode mode();

    boolean forceCloseDialogAfterToast();

    int socketTimeout();
}
