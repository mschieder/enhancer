package org.openjpa.ide.idea.config.swing;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.openjpa.ide.idea.config.MetaDataOrClassFile;

/**
 */
public class MetadataOrClassFilesRowModel extends AbstractTableModel {

    @Serial
    private static final long serialVersionUID = 1L;
    private final List<MetaDataOrClassFile> files = new ArrayList<>();

    public MetadataOrClassFilesRowModel(final Collection<MetaDataOrClassFile> metadataFiles,
                                        final Collection<MetaDataOrClassFile> annotatedFiles) {
        if (metadataFiles != null && !metadataFiles.isEmpty()) {
            this.files.addAll(metadataFiles);
        }
        if (annotatedFiles != null && !annotatedFiles.isEmpty()) {
            this.files.addAll(annotatedFiles);
        }
    }

    public List<MetaDataOrClassFile> getFiles() {
        return files;
    }

    @Override
    public int getRowCount() {
        return this.files.size();
    }

    @Override
    public int getColumnCount() {
        return 5;
    }

    @Override
    public String getColumnName(final int columnIndex) {
        return switch (columnIndex) {
            case (0) -> "Enabled";
            case (1) -> "Module";
            case (2) -> "Class";
            case (3) -> "File";
            case (4) -> "Path";
            default -> throw new IllegalArgumentException("invalid column index for retrieving name: " + columnIndex);
        };
    }


    @Override
    public boolean isCellEditable(final int rowIndex, final int columnIndex) {
        return columnIndex == 0;
    }

    @Override
    public Class<?> getColumnClass(final int columnIndex) {
        return columnIndex == 0 ? Boolean.class : String.class;
    }

    @SuppressWarnings("FeatureEnvy")
    @Override
    public Object getValueAt(final int rowIndex, final int columnIndex) {
        final MetaDataOrClassFile moc = this.files.get(rowIndex);

        final String ret;
        switch (columnIndex) {
            case (0):
                return moc.isEnabled();
            case (1):
                ret = moc.getModuleName();
                break;
            case (2):
                ret = moc.getClassName();
                break;
            case (3):
                ret = moc.getFileName();
                break;
            case (4):
                ret = moc.getPath();
                break;
            default:
                throw new IllegalArgumentException("invalid column index for retrieving value: " + columnIndex);
        }
        return ret;
    }

    @Override
    public void setValueAt(final Object aValue, final int rowIndex, final int columnIndex) {
        if (columnIndex == 0) {
            final MetaDataOrClassFile classFile = this.files.get(rowIndex);
            classFile.setEnabled((Boolean) aValue);
            this.fireTableCellUpdated(rowIndex, columnIndex);
        }
    }


}
