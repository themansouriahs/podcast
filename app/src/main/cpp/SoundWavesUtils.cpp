float lagrangeInterpolatingPolynomial( float pos[], float val[], int degree, float desiredPos )  {
    float retVal = 0;

    for ( int i = 0; i < degree; ++i ) {
        float weight = 1;

        for ( int j = 0; j < degree; ++j ) {
            // The i-th term has to be skipped
            if ( j != i ) {
                weight *= (desiredPos - pos[j]) / (pos[i] - pos[j]);
            }
        }

        retVal += weight * val[i];
    }

    return retVal;
}