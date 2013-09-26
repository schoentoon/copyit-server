/*  copyit-server
 *  Copyright (C) 2013  Toon Schoenmakers
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.mms_projects.copy_it.server;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import net.mms_projects.copy_it.server.config.MissingKey;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;

public class LanguageHeader implements List<Locale> {
    public static LanguageHeader fromRequest(final HttpRequest request) {
        String header = request.headers().get(HttpHeaders.Names.ACCEPT_LANGUAGE);
        if (header == null)
            return EMPTY;
        return new LanguageHeader(header);
    }

    private static final LanguageHeader EMPTY = new LanguageHeader();

    private LanguageHeader() {
        locales = new ArrayList<Locale>(0);
    }

    public LanguageHeader(final String header) {
        locales = new ArrayList<Locale>();
        for (String l : header.split(",")) {
            try {
                String[] arr = l.trim().split(";");
                String[] lo = arr[0].split("-");
                Locale locale = null;
                switch (lo.length) {
                    case 2: locale = new Locale(lo[0], lo[1]); break;
                    case 3: locale = new Locale(lo[0], lo[1], lo[2]); break;
                    default: locale = new Locale(lo[0]); break;
                }
                locales.add(locale);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static final String DOT = ".";
    private static final String UNDERSCORE = "_";
    //private static final String DOT_DEFAULT = ".en_US";

    public String getPageInLocale(final String page_name) throws IOException, MissingKey {
        Iterator<Locale> iter = locales.iterator();
        while (iter.hasNext()) {
            Locale locale = iter.next();
            try {
                if (locale.getCountry().isEmpty())
                    return FileCache.get(page_name + DOT + locale.getLanguage());
                return FileCache.get(page_name + DOT + locale.getLanguage() + UNDERSCORE + locale.getCountry());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        //return FileCache.get(page_name + DOT_EN); //TODO Where is my english page damn it :U
        return FileCache.get(page_name);
    }

    @Override
    public int size() {
        return locales.size();
    }

    @Override
    public boolean isEmpty() {
        return locales.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return locales.contains(o);
    }

    @Override
    public Iterator<Locale> iterator() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object[] toArray() {
        return locales.toArray();
    }

    @Override
    public <T> T[] toArray(T[] ts) {
        return locales.toArray(ts);
    }

    @Override
    public boolean add(Locale locale) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsAll(Collection<?> objects) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(Collection<? extends Locale> locales) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(int i, Collection<? extends Locale> locales) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> objects) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> objects) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Locale get(int i) {
        return locales.get(i);
    }

    @Override
    public Locale set(int i, Locale locale) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void add(int i, Locale locale) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Locale remove(int i) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int indexOf(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int lastIndexOf(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListIterator<Locale> listIterator() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListIterator<Locale> listIterator(int i) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Locale> subList(int i, int i2) {
        throw new UnsupportedOperationException();
    }

    private final List<Locale> locales;
}
