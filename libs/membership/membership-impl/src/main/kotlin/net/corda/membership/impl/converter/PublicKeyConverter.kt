package net.corda.membership.impl.converter

import net.corda.layeredpropertymap.ConversionContext
import net.corda.layeredpropertymap.CustomPropertyConverter
import net.corda.v5.cipher.suite.KeyEncodingService
import org.osgi.service.component.annotations.Activate
import org.osgi.service.component.annotations.Component
import org.osgi.service.component.annotations.Reference
import java.security.PublicKey

/**
 * Converter class, converting from String to [PublicKey] object.
 *
 * @property keyEncodingService to convert the strings into PublicKeys
 */
@Component(service = [CustomPropertyConverter::class])
class PublicKeyConverter @Activate constructor(
    @Reference(service = KeyEncodingService::class)
    private val keyEncodingService: KeyEncodingService
) : CustomPropertyConverter<PublicKey> {
    override val type: Class<PublicKey>
        get() = PublicKey::class.java

    /**
     * Select the single element in case the structure is like 'corda.identityKeys.1'
     */
    override fun convert(context: ConversionContext): PublicKey? =
        context.value()?.let { keyEncodingService.decodePublicKey(it) }
}