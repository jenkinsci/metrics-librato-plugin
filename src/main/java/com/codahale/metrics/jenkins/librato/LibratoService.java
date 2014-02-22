package com.codahale.metrics.jenkins.librato;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.concurrent.GuardedBy;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Cyrille Le Clerc
 */
public class LibratoService extends AbstractDescribableImpl<LibratoService> {

    private static final Logger LOGGER = Logger.getLogger(LibratoService.class.getName());

    private final String username;

    private final String token;

    private final String source;

    private final String prefix;

    @DataBoundConstructor
    public LibratoService(String username, String token, String source, String prefix) {
        this.username = username;
        this.token = token;
        this.source = source;
        this.prefix = prefix;
    }

    public String getUsername() {
        return username;
    }

    public String getToken() {
        return token;
    }

    public String getSource() {
        return source;
    }

    public String getPrefix() {
        return prefix;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("LibratoService{");
        sb.append("username='").append(username).append('\'');
        sb.append(", token=").append(token);
        sb.append(", source='").append(source).append('\'');
        sb.append(", prefix='").append(prefix).append('\'');
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        LibratoService that = (LibratoService) o;

        if (token != that.token) {
            return false;
        }
        if (username != null ? !username.equals(that.username) : that.username != null) {
            return false;
        }
        if (source != null ? !source.equals(that.source) : that.source != null) {
            return false;
        }
        if (prefix != null ? !prefix.equals(that.prefix) : that.prefix != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = username != null ? username.hashCode() : 0;
        result = 31 * result + (token != null ? token.hashCode() : 0);
        result = 31 * result + (source != null ? source.hashCode() : 0);
        result = 31 * result + (prefix != null ? prefix.hashCode() : 0);
        return result;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<LibratoService> {
        @GuardedBy("this")
        private List<LibratoService> services;

        @Override
        public String getDisplayName() {
            return Messages.LibratoService_displayName();
        }

        public DescriptorImpl() {
            super();
            load();
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            setServices(req.bindJSONToList(LibratoService.class, json.get("services")));
            return true;
        }

        public synchronized List<LibratoService> getServices() {
            return services == null
                    ? Collections.<LibratoService>emptyList()
                    : Collections.unmodifiableList(new ArrayList<LibratoService>(services));
        }

        public synchronized void setServices(List<LibratoService> services) {
            this.services = services;
            save();
            try {
                Jenkins.getInstance().getPlugin(PluginImpl.class).updateReporters();
            } catch (URISyntaxException e) {
                LOGGER.log(Level.WARNING, "Could not update Librato reporters", e);
            }
        }
    }

}
