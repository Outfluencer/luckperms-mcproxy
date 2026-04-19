/*
 * This file is part of LuckPerms, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package me.lucko.luckperms.mcproxy;

import me.lucko.luckperms.common.plugin.classpath.ClassPathAppender;
import sun.misc.Unsafe;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

public class MCProxyClassPathAppender implements ClassPathAppender {
    private final URLClassLoader classLoader;
    private final MethodHandle addUrlHandle;

    public MCProxyClassPathAppender(LPMCProxyBootstrap bootstrap) {
        ClassLoader cl = bootstrap.getClass().getClassLoader();
        if (!(cl instanceof URLClassLoader)) {
            throw new RuntimeException("ClassLoader is not an instance of URLClassLoader");
        }
        this.classLoader = (URLClassLoader) cl;

        try {
            // Use Unsafe to obtain a trusted MethodHandles.Lookup that can access
            // URLClassLoader.addURL on modern Java where module access is restricted.
            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            Unsafe unsafe = (Unsafe) unsafeField.get(null);

            Field implLookupField = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
            long offset = unsafe.staticFieldOffset(implLookupField);
            MethodHandles.Lookup trustedLookup = (MethodHandles.Lookup) unsafe.getObject(MethodHandles.Lookup.class, offset);

            this.addUrlHandle = trustedLookup
                    .findVirtual(URLClassLoader.class, "addURL", MethodType.methodType(void.class, URL.class));
        } catch (Exception e) {
            throw new RuntimeException("Failed to obtain access to URLClassLoader#addURL", e);
        }
    }

    @Override
    public void addJarToClasspath(Path file) {
        try {
            this.addUrlHandle.invoke(this.classLoader, file.toUri().toURL());
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
