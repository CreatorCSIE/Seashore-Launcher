/*
 * Hello Minecraft! Launcher
 * Copyright (C) 2020  huangyuhui <huanghongxun2008@126.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.tungsten.fclcore.util;

import org.jetbrains.annotations.NotNull;

/**
 * @author huangyuhui
 */
// ★ Phase 1.5：原 FCL 用 Java 16 record；QCL 模块 source/target=1.8，record 不可用
//   （且 minSdk 26 运行时无 java.lang.Record），改写为等价的 final 类，保留全部访问器。
public final class Pair<K, V> {

    private final K key;
    private final V value;

    public Pair(K key, V value) {
        this.key = key;
        this.value = value;
    }

    public static <K, V> Pair<K, V> pair(K key, V value) {
        return new Pair<>(key, value);
    }

    public K key() {
        return key;
    }

    public V value() {
        return value;
    }

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Pair)) return false;
        Pair<?, ?> other = (Pair<?, ?>) o;
        return java.util.Objects.equals(key, other.key)
                && java.util.Objects.equals(value, other.value);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(key, value);
    }

    @Override
    public @NotNull String toString() {
        return "(" + key + ", " + value + ")";
    }
}