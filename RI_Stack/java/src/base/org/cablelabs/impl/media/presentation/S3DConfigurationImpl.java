package org.cablelabs.impl.media.presentation;

import org.ocap.media.S3DConfiguration;

/**
 *  This interface represents a 3D frame packing payload as defined in [OCCEP].
 */
public class S3DConfigurationImpl implements S3DConfiguration
{
    private int m_payloadType;  // type of payload
    private int m_formatType;
    private byte[] m_payload;

    public S3DConfigurationImpl(int payloadType, int formatType, byte[] payload)
    {
        m_payloadType = payloadType;
        m_formatType = formatType;
        m_payload = payload;
    }

    /**
     * Gets the data type of the 3D content.
     * Returns <code>S3D_MPEG2_USER_DATA_TYPE</code> when the stream type is
     *      <code>MPEG2_VIDEO</code>, or <code>S3D_AVC_SEI_PAYLOAD_TYPE</code>
     *      when the stream type is <code>AVC_VIDEO</code>.  Note: other data
     *      types may be added in the future.
     *
     * @return The data type of the 3D content.
     */
    public int getDataType()
    {
        return m_payloadType;
    }

    /**
     * Gets the 3D content format type.  See <code>S3DFormatTypes</code>
     * for possible return values.
     *
     * @return The 3D content format type.
     */
    public int getFormatType()
    {
        return m_formatType;
    }

    /**
     * Gets the payload of the 3DTV information description message.  The byte
     * array format will match the definition for the data type returned by the
     * <code>getDataType</code> method.
     */
    public byte [] getPayload()
    {
        return m_payload;
    }
}
