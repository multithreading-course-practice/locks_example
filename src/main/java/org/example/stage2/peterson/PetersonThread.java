package org.example.stage2.peterson;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class PetersonThread extends Thread {

    // идентификатор потока, может быть равен 0 или 1
    private final int petersonID;
}
