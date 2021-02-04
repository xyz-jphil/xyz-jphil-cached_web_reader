/*
 * Copyright 2021 Ivan Velikanova ivan.velikanova@gmail.com .
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.xyzjphil.xyz.jphil.cached_web_reader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static java.nio.file.StandardOpenOption.*;
import java.util.Optional;
import java.util.function.Consumer;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;



/**
 *
 * @author
 */
public class ReadWeb {
    private final String url;
    private boolean refreshCache;
    private Consumer<Exception> onException;
    private Response response;
    private Document cachedResult;
    private boolean cache;
    private boolean fresh;
    private long dataFetchTimestamp;

    public ReadWeb(String url) {
        this.url = url;
    }
    
    public Optional<Document> document(){
        return readUrl(url, 0, refreshCache, onException);
    }
    
    public boolean dataExists(){
        return cachedResult!=null;
    }

    public boolean isCache() {
        return cache;
    }

    public boolean isFresh() {
        return fresh;
    }

    public java.util.Date dataFetchTimestamp() {
        return new java.util.Date(dataFetchTimestamp);
    }
    
    public static ReadWeb read(String link){
        return new ReadWeb(link);
    }
    
    public static final Optional<Path> localCachePath(String url, Consumer<Exception> onException){
        Path uh = Paths.get(System.getProperty("user.home"));
        Path cache = uh.resolve(".jphil").resolve("xyz-jphil-cached_web_reader").resolve("cache");
        try{
            if(!Files.exists(cache)){
                Files.createDirectories(cache);
            }
        }catch(Exception a){
            if(onException!=null)onException.accept(a);
            else a.printStackTrace();
            return Optional.empty();
        }
        url = url.replace("http://", "");
        url = url.replace("https://", "");        
        Path p = cache.resolve(url+".cache.html");
        
        try{
            if(!Files.exists(p.getParent())){
                Files.createDirectories(p.getParent());
            }
        }catch(Exception a){
            if(onException!=null)onException.accept(a);
            else a.printStackTrace();
        }
        
        return Optional.of(p);
    }
    
    public static final Optional<Document> readUrl(String url, int expectedMinSize, boolean refreshCache, Consumer<Exception> onException){
        return readUrlImpl(url, expectedMinSize, refreshCache, onException, null);
    }
    private static final Optional<Document> readUrlImpl(String url, int expectedMinSize, 
            boolean refreshCache, Consumer<Exception> onException, ReadWeb rw){
        url = url.replace("//", "/");
        url = url.replace("//", "/");
        url = url.replace("http:/", "http://"); //put back
        url = url.replace("https:/", "https://");
        try {
            Path localCachePath = localCachePath(url, null).orElse(null);
            if(localCachePath==null)System.out.println("Could not construct local path for "+url);
            if(localCachePath!=null && Files.exists(localCachePath) 
                    && Files.size(localCachePath)>expectedMinSize && !refreshCache){ 
                Document d = Jsoup.parse(localCachePath.toFile(), "UTF-8");
                if(rw!=null){
                    rw.cachedResult = d;
                    rw.dataFetchTimestamp = Files.getLastModifiedTime(localCachePath).toMillis();
                    rw.cache = true;
                    rw.fresh = false;
                }
                return Optional.of(d);
            }
            System.out.println("Reading from web "+url);
            Response response = Request.Get(url).execute();
            if(rw!=null){
                rw.response = response;
            }
            String html = response.returnContent().asString();
            Document d = Jsoup.parse(html);
            try {
                Files.writeString(localCachePath, html, WRITE, CREATE);
            } catch (Exception e) {
                if(onException!=null)onException.accept(e);
                else e.printStackTrace();
            }
            if(rw!=null){
                rw.cachedResult = d;
                rw.dataFetchTimestamp = System.currentTimeMillis();
                rw.cache = false;
                rw.fresh = true;
            }
            return Optional.of(d);
        } catch (Exception e) {
            if(onException!=null)onException.accept(e);
            else e.printStackTrace();
        }
        return Optional.empty();
    }
}
