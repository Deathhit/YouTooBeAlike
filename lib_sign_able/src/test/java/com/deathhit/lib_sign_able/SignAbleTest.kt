package com.deathhit.lib_sign_able

import org.junit.Test

class SignAbleTest {
    @Test
    fun testSign() {
        val content = "123"
        val signature1 = "456"
        val signature2 = "789"

        val signAble = SignAble(content)

        var isSigned = false
        signAble.sign(signature1) {
            assert(it == content)
            isSigned = true
        }

        assert(isSigned)

        isSigned = false
        signAble.sign(signature1) {
            assert(it == content)
            isSigned = true
        }

        assert(!isSigned)

        isSigned = false
        signAble.sign(signature2) {
            assert(it == content)
            isSigned = true
        }

        assert(isSigned)
    }
}