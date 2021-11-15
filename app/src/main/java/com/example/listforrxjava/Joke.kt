package com.example.listforrxjava

import java.io.Serializable

class Joke(private val id: Int, private val joketext : String) : Serializable
{
    public fun getId() : Int {return id}
    public fun getJokeText() : String {return joketext}
}