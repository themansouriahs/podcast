package org.bottiger.podcast.player.exoplayer;

/**
 * Created by aplb on 15-06-2016.
 */

class SilenceRemover {

    private static final int SPEECH_BUFFER = 2000;
    private static final double THRESHOLD =  400; //0.02;

    private final int sampleRate;
    private final int numChannels;

    private int sampleCapacity = 0;
    private int mOutputIndex;

    private int silence_start = -2; // convenient initial condition
    private int silence_end = -1;

    private short[] input_signal_2;
    private short[] output_signal_2;

    // Create a sonic stream.
    SilenceRemover(
            int sampleRate,
            int numChannels) {
        this.sampleRate = sampleRate;
        this.numChannels = numChannels;
    }

    int getOutputLength() {
        return mOutputIndex;
    }

    byte[] removeSilence(byte[] input_signal, byte[] output_signal, int argBytesToRead) {
        int i;

        int numSamples = argBytesToRead / (2 * numChannels);
        short sample;

        int buffer_size = (argBytesToRead << 1);

        if (buffer_size > output_signal.length) {
            input_signal_2 = new short[buffer_size];
            output_signal_2 = new short[buffer_size];

            output_signal = resize(output_signal, buffer_size);
            sampleCapacity = buffer_size;
        }

        int numInputSamples = 0; //numSamples;

        int xBuffer = numInputSamples * numChannels;
        for (int xByte = 0; xByte + 1 < argBytesToRead; xByte += 2) {
            sample = (short) ((input_signal[xByte] & 0xff) | (input_signal[xByte + 1] << 8));
            input_signal_2[xBuffer++] = sample;
        }
        numInputSamples += numSamples;


        mOutputIndex = 0;

        double input_value;
        boolean is_voiced;

        int loop_size = xBuffer;

        for (i = 0; i < loop_size; i++) {

            input_value = Math.abs(input_signal_2[i]);
            is_voiced = input_value >= THRESHOLD;

            // Detect if we are moving into speech
            if (is_voiced && silence_start > silence_end) {
                silence_end = i;
            }

            // Detect if we are moving into silence
            if (!is_voiced && silence_start < silence_end) {
                silence_start = i;
            }

            // There have been silence for a long time
            int silence_length = i - silence_start;
            boolean is_long_pause = !is_voiced && silence_start > silence_end && silence_length > SPEECH_BUFFER;
            if (!is_long_pause) {
                output_signal_2[mOutputIndex] = input_signal_2[i];
                mOutputIndex++;
            }

        }

        int outCount = 0;
        for (int xSample = 0; xSample < mOutputIndex; xSample++) {
            short sample_out = output_signal_2[xSample];
            output_signal[xSample << 1] = (byte) (sample_out & 0xff);
            output_signal[(xSample << 1) + 1] = (byte) (sample_out >> 8);
            outCount = (xSample << 1) + 1;
        }

        mOutputIndex *= 2;

        silence_start -= loop_size;
        silence_end -= loop_size;

        return output_signal;
    }

    // Resize the array.
    private byte[] resize(
            byte[] oldArray,
            int newLength) {
        newLength *= numChannels;
        byte[] newArray = new byte[newLength];
        int length = oldArray.length <= newLength ? oldArray.length : newLength;

        System.arraycopy(oldArray, 0, newArray, 0, length);
        return newArray;
    }
}
