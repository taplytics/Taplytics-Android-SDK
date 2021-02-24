package com.taplytics.sdk.analytics.external

import com.taplytics.sdk.analytics.external.adobe.AdobeManager
import com.taplytics.sdk.utils.TLLog
import io.mockk.every
import io.mockk.spyk
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import java.util.*

@RunWith(PowerMockRunner::class)
@PrepareForTest(TLLog::class)
class AdobeManagerTest {

    lateinit var adobeManager: AdobeManager

    @Before
    fun setup() {
        adobeManager = spyk()
    }

    @Test
    fun `Baseline A Format`() {
        val map = hashMapOf<String, String>()
        map["exp"] = "baseline"
        map["exp2"] = "B"

        adobeManager.format = AdobeManager.Format.BASELINE_A
        val formattedMap = adobeManager.format.apply(map)

        val valid = listOf(
                "exp2:B, exp:A",
                "exp:A, exp2:B"
        )
        assert(valid.contains(formattedMap["tl_exp"]));
    }

    @Test
    fun `Exp Format`() {
        val map = HashMap<String, String>()
        map["exp"] = "baseline"
        map["exp2"] = "B"

        adobeManager.format = AdobeManager.Format.TL_EXP_1

        val formattedMap = adobeManager.format.apply(map)

        val valid: MutableMap<String, String> = HashMap()
        valid["experiment_2"] = "exp | baseline"
        valid["experiment_1"] = "exp2 | B"
        valid["tl_exp"] = "1"

        assert(formattedMap.containsKey("experiment_1"))
        Assert.assertEquals(valid["experiment_1"], formattedMap["experiment_1"])

        assert(formattedMap.containsKey("experiment_2"))
        Assert.assertEquals(formattedMap["experiment_2"], valid["experiment_2"])

        assert(formattedMap.containsKey("tl_exp"))
        Assert.assertEquals(formattedMap["tl_exp"], valid["tl_exp"])
    }

    @Test
    fun `Format With No Data`() {
        val map = HashMap<String, String>()

        adobeManager.format = AdobeManager.Format.BASELINE_A
        val formattedMap = adobeManager.format.apply(map)

        Assert.assertEquals("", formattedMap["tl_exp"])
    }

    @Test
    fun `Format With Null Data`() {
        adobeManager.format = AdobeManager.Format.BASELINE_A
        val formattedMap = adobeManager.format.apply(null)

        Assert.assertEquals(null, formattedMap["tl_exp"])
    }

}