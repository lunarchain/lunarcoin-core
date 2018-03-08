package io.lunarchain.lunarcoin.core

import lunar.vm.DataWord
import kotlin.collections.HashMap

class AccountStorage(val address: ByteArray, val storage: HashMap<DataWord, DataWord?>) {
}