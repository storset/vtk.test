/* Copyright (c) 2008, University of Oslo, Norway
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 
 *  * Neither the name of the University of Oslo nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *      
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.vortikal.web.controller.feed;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.model.Link;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.util.URIUtil;
import org.apache.commons.lang.StringUtils;
import org.openxri.IRIUtils;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.HtmlValueFormatter;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.repository.resourcetype.ValueFormatter;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.service.Service;

public abstract class AtomFeedController implements Controller {

    private static final String TAG_PREFIX = "tag:";
    protected static final Namespace NS = Namespace.DEFAULT_NAMESPACE;

    protected Repository repository;
    protected Service viewService;
    protected Abdera abdera;

    protected PropertyTypeDefinition authorPropDef;
    protected PropertyTypeDefinition publishedDatePropDef;


    protected abstract Feed createFeed(HttpServletRequest request, HttpServletResponse response,
            String token) throws Exception;


    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        String token = SecurityContext.getSecurityContext().getToken();

        Feed feed = createFeed(request, response, token);

        if (feed != null) {
            response.setContentType("application/atom+xml;charset=utf-8");
            feed.writeTo("prettyxml", response.getWriter());
        }

        return null;

    }


    protected Feed populateFeed(Resource collection, String feedTitle) throws IOException,
            URIException, UnsupportedEncodingException {

        Feed feed = abdera.newFeed();
        Property published = collection.getProperty(NS, PropertyType.CREATIONTIME_PROP_NAME);
        feed.setId(getId(collection.getURI(), published, getFeedPrefix()));
        feed.addLink(viewService.constructLink(collection.getURI()), "alternate");
        
        if (Path.ROOT.equals(collection.getURI())) {
            feedTitle = this.repository.getId();
        }
        feed.setTitle(feedTitle);

        String subTitle = getIntroduction(collection);
        if (subTitle != null) {
            feed.setSubtitleAsXhtml(subTitle);
        } else {
            subTitle = getDescription(collection);
            if (subTitle != null) {
                feed.setSubtitle(subTitle);
            }
        }

        Property picture = getPicture(collection);
        if (picture != null) {
            String val = picture.getFormattedValue("thumbnail", Locale.getDefault());
            feed.setLogo(val);
        }

        Date lastModified = getLastModified(collection);
        if (lastModified != null) {
            feed.setUpdated(lastModified);
        }
        return feed;
    }


    protected void populateEntry(String token, PropertySet result, Entry entry)
            throws URIException, UnsupportedEncodingException {

        String id = getId(result.getURI(), result.getProperty(NS,
                PropertyType.CREATIONTIME_PROP_NAME), null);
        entry.setId(id);
        entry.addCategory(result.getResourceType());

        Property prop = result.getProperty(NS, PropertyType.TITLE_PROP_NAME);
        entry.setTitle(prop.getFormattedValue());

        Property type = result.getProperty(NS, PropertyType.XHTML_PROP_NAME);
        // Add introduction and/or pic as xhtml if resource is event or
        // article...
        if (type != null
                && (StringUtils.equals(type.getStringValue(), "event") || StringUtils.equals(type
                        .getStringValue(), "article"))) {

            String summary = prepareSummary(result);
            entry.setSummaryAsXhtml(summary);
            // ...add description as plain text else
        } else {
            String description = getDescription(result);
            if (description != null) {
                entry.setSummary(description);
            }
        }

        if (this.publishedDatePropDef != null) {
            prop = result.getProperty(this.publishedDatePropDef);
            entry.setPublished(prop.getDateValue());
        }

        prop = result.getProperty(NS, PropertyType.LASTMODIFIED_PROP_NAME);
        entry.setUpdated(prop.getDateValue());

        if (this.authorPropDef != null) {
            prop = result.getProperty(this.authorPropDef);
            if (prop != null) {
                ValueFormatter vf = prop.getDefinition().getValueFormatter();
                if (prop.getDefinition().isMultiple()) {
                    for (Value v : prop.getValues()) {
                        entry.addAuthor(vf.valueToString(v, "name", null));
                    }
                } else {
                    entry.addAuthor(prop.getFormattedValue("name", null));
                }
            }
        }

        prop = result.getProperty(NS, PropertyType.MEDIA_PROP_NAME);
        if (prop != null) {
            try {
                Link link = abdera.getFactory().newLink();
                Path propRef = getPropRef(result, prop.getStringValue());
                link.setHref(viewService.constructLink(propRef));
                link.setRel("enclosure");
                Resource mediaResource = repository.retrieve(token, propRef, true);
                link.setMimeType(mediaResource.getContentType());
                entry.addLink(link);
            } catch (Throwable t) {
            }
        }

        try {
            Link link = abdera.getFactory().newLink();
            link.setHref(viewService.constructLink(result.getURI()));
            link.setRel("alternate");
            entry.addLink(link);
        } catch (Throwable t) {
        }
    }


    protected String prepareSummary(PropertySet resource) {
        StringBuilder sb = new StringBuilder();
        String summary = getIntroduction(resource);
        Property pic = resource.getProperty(NS, PropertyType.PICTURE_PROP_NAME);
        if (pic != null) {
            String imageRef = pic.getStringValue();
            if (!imageRef.startsWith("/") && !imageRef.startsWith("https://")
                    && !imageRef.startsWith("https://")) {
                try {
                    imageRef = resource.getURI().getParent().extendAndProcess(imageRef).toString();
                    pic.setValue(new Value(imageRef));
                } catch (Throwable t) {
                }
            }
            String imgPath = pic.getFormattedValue("thumbnail", Locale.getDefault());
            String imgAlt = getImageAlt(imgPath);
            sb.append("<img src=\"" + imgPath + "\" alt=\"" + imgAlt + "\"/>");
        }
        if (summary != null) {
            sb.append(summary);
        }
        return sb.toString();
    }


    protected String getIntroduction(PropertySet resource) {
        Property prop = resource.getProperty(NS, PropertyType.INTRODUCTION_PROP_NAME);
        return prop != null ? prop.getFormattedValue() : null;
    }


    protected String getDescription(PropertySet resource) {
        Namespace NS_CONTENT = Namespace.getNamespace("http://www.uio.no/content");
        Property prop = resource.getProperty(NS_CONTENT, PropertyType.DESCRIPTION_PROP_NAME);
        return prop != null ? prop.getFormattedValue(HtmlValueFormatter.FLATTENED_FORMAT, null)
                : null;
    }


    protected Path getPropRef(PropertySet resource, String val) {
        if (val.startsWith("/")) {
            return Path.fromString(val);
        }
        return resource.getURI().extend(val);
    }


    protected String getId(Path resourceUri, Property published, String prefix)
            throws URIException, UnsupportedEncodingException {
        String host = viewService.constructURL(resourceUri).getHost();
        StringBuilder sb = new StringBuilder(TAG_PREFIX);
        sb.append(host + ",");
        sb.append(published.getFormattedValue("iso-8601-short", null) + ":");
        if (prefix != null) {
            sb.append(prefix);
        }
        String uriString = resourceUri.toString();
        // Remove any invalid character before decoding
        uriString = removeInvalid(uriString);
        uriString = URIUtil.decode(uriString);
        // Remove any unknown character after decoding
        uriString = removeInvalid(uriString);
        uriString = URIUtil.encode(uriString, null);
        String iriString = IRIUtils.URItoIRI(uriString);
        sb.append(iriString);
        return sb.toString();
    }


    protected String getFeedPrefix() {
        return null;
    }


    protected Date getLastModified(Resource collection) {
        return collection.getLastModified();
    }


    protected Property getPicture(Resource collection) {
        return collection.getProperty(NS, PropertyType.PICTURE_PROP_NAME);
    }


    private String removeInvalid(String s) {
        return s.replaceAll("[#%?\\[\\] ]", "");
    }


    private String getImageAlt(String imgPath) {
        try {
            return imgPath.substring(imgPath.lastIndexOf("/") + 1, imgPath.lastIndexOf("."));
        } catch (Throwable t) {
            // Don't do anything special, imgAlt isn't all that important
            return "feed_image";
        }
    }


    @Required
    public void setRepository(Repository repository) {
        this.repository = repository;
    }


    @Required
    public void setViewService(Service viewService) {
        this.viewService = viewService;
    }


    @Required
    public void setAbdera(Abdera abdera) {
        this.abdera = abdera;
    }


    public void setAuthorPropDef(PropertyTypeDefinition authorPropDef) {
        this.authorPropDef = authorPropDef;
    }


    public void setPublishedDatePropDef(PropertyTypeDefinition publishedDatePropDef) {
        this.publishedDatePropDef = publishedDatePropDef;
    }

}
