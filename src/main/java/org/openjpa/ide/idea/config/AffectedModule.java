package org.openjpa.ide.idea.config;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 */
public class AffectedModule implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private boolean enabled;

    private final String name;

    public AffectedModule(final boolean enabled, final String name) {
        this.enabled = enabled;
        this.name = name;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    public String getName() {
        return this.name;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final AffectedModule that = (AffectedModule) o;

        if (this.enabled != that.enabled) {
            return false;
        }
        return Objects.equals(this.name, that.name);
    }

    @Override
    public int hashCode() {
        int result = (this.enabled ? 1 : 0);
        result = 31 * result + (this.name != null ? this.name.hashCode() : 0);
        return result;
    }

    @SuppressWarnings("MagicCharacter")
    @Override
    public String toString() {
        return "AffectedModule{" + "enabled=" + this.enabled + ", name='" + this.name + '\'' + '}';
    }

}
